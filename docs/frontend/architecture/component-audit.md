# Auditoría de componentes React — 2026-10-22

## Alcance y decisiones

Se inspeccionó todo `frontend/src/`: 54 componentes presentes al retomar (51 componentes en archivos propios y tres funciones React privadas en `PlayerCard`), más dos componentes recuperados: **56 componentes**. `main.tsx` es bootstrap, no un componente. Los iconos importados de Lucide no son implementaciones propias.

Todos tienen función nombrada con export default, barrel local default e interfaz local `Props` únicamente cuando reciben propiedades. Los tres componentes internos de `PlayerCard` ahora tienen módulo propio sin agregar nodos al DOM. Se conservaron las declaraciones CSS; se normalizaron identificadores de clases y sus referencias a camelCase/BEM y notación de punto.

### Inventario y propietarios

| Ámbito | Componentes y composición efectiva |
| --- | --- |
| Composición global | `App` compone `AppRouter`; `AppRouter` configura las Pages y layouts de ruta. Su ubicación en router conserva esa responsabilidad global. |
| Layouts | `AppLayout`, `AuthGuard`, `PageLayout`, `PaginatedPageLayout`; `PageLayout` es compartido por resumen y detalle, y por el layout paginado. `AuthLayout` global recuperado permanece sin uso. |
| Providers | `AppProviders`, con `HttpCredentials` y `SessionLifecycle` privados. `main.tsx` consume el provider público. |
| Shared | `Button` (formularios, paginación, navegación, logout y detalle), `Field` (login/registro), `Pagination` (layout paginado), `Navbar` (contrato genérico de navegación). |
| Navbar | `NavIcon` y `TabLink` privados; `SoonBadge` compartido por Navbar y TabLink, conservado en Navbar como ancestro común. |
| Auth | `AuthLayout` compartido por `LoginPage` y `RegisterPage`; `AuthVisualPanel` privado de ese layout. `SessionChecking` conserva la frontera de auth y su API consumida por `AuthGuard`. |
| Pages auth | `HomePage`, `LoginPage`, `RegisterPage`; `LogoutButton` privado de HomePage. |
| Catálogo | `PlayersPage` posee `PlayerCatalogControls`, `PlayerCard` y `PlayersPagination` recuperado sin uso. PlayerCard posee su `PlayerPortrait`, `NationalityFlag` y `TeamBadge`. |
| Detalle jugador | `PlayerDetailPage` posee `PlayerBreadcrumb`, `PlayerOverview`, `PlayerQuoteEvolutionChart`, `PlayerQuotePanel`, `PlayerSportsSummary`. PlayerOverview posee su propio `PlayerPortrait`, diferente del retrato del catálogo. |
| Tenencias | `PortfolioHoldingsPage` posee Header, Content y `PortfolioPositionDetail` sin uso; Header posee `PortfolioStatusFilter`; Content posee `PortfolioPositionCard`. `PortfolioPortrait` permanece en la Page, nivel común entre tarjeta y detalle. |
| Portfolio compartido | `PortfolioValueSection` se comparte entre Panels y PositionDetail; posee `PortfolioValueHeader` y `PortfolioValueRows`; Rows posee `PortfolioValueRow`. |

### Recuperación de eliminaciones anteriores

Se recuperaron de HEAD los archivos de `app/layouts/AuthLayout` y `PlayersPagination`, eliminados por el agente anterior sin autorización específica. Se conservan sus implementaciones sin conectarlas a rutas ni sustituir la paginación actual. `PortfolioPositionDetail` nunca fue eliminado realmente: el informe anterior era incorrecto. Se conserva, sin activarlo. Las antiguas rutas de componentes movidos no se restauran como duplicados. El recurso genérico de jugador conserva su ubicación compartida y consumidores actuales.

### Prevención y límites de automatización

Biome conserva su configuración: no se aplican prohibiciones globales de named exports, pues afectarían Hooks, tipos y utilidades válidos. Las herramientas configuradas no aportan una regla completa que reconozca semánticamente Component + propietario + props + barrel. No se añadió un detector React basado en regex ni dependencias nuevas.

La revisión debe seguir los artículos 2.º, 8.º y 3.º de arquitectura. Una futura automatización fiable requiere análisis AST y resolución de símbolos/imports sobre un inventario explícito de componentes; incluso así, la propiedad arquitectónica exige revisión humana. Esa integración queda propuesta, no implementada ni ejecutada.

El script existente `scss:check-imports` no se ejecutó ni se presenta como garantía completa: su patrón actual solo reconoce determinados imports al comienzo del archivo. Endurecerlo con análisis de directivas SCSS que distinga comentarios y strings queda pendiente; no se confunde con análisis de React.

## Verificación

Solo se ejecuta `npm run build` conforme a Constitución y `frontend/AGENTS.md`. Incluye TypeScript y compilación Sass/Vite, pero no prueba visual ni detección exhaustiva de ciclos. Biome, tests, `scss:check-imports` y un análisis automático de ciclos quedan pendientes. La revisión de imports, consumidores y barrels se realizó mediante lectura y búsquedas; no se declara ejecución de esos controles.

### Corrección posterior de las tres inconsistencias

La revisión final detectó clases raíz no emitidas en Header y Content del resumen, selectores de elementos concatenados al modificador flotante y una observación documental que negaba la existencia del AuthLayout global recuperado. El inventario arquitectónico no acreditaba la correspondencia de todos los selectores ni equivalencia visual.

- Header no tenía declaraciones raíz necesarias: se retira únicamente su referencia de clase, conservando el elemento `header` y los estilos de título y descripción.
- Content perdió durante la extracción sus declaraciones de composición originales: columna flex, `gap: 14px` y `margin-top: 19px`. Se restituyen al contenedor existente, sin wrappers nuevos ni cambios en las tarjetas.
- El nesting de PageLayout producía clases `layout_floating__footer/content` que JSX no aplicaba. El error precedía al renombrado de clases: se corrige a descendientes `.layout_floating .layout__footer/content`, dentro del mismo modo compacto. El modificador solo se aplica cuando existe footer, evitando reservar una banda vacía durante carga/error o sin paginación.
- La descripción de AuthLayout se actualiza tanto en el índice arquitectónico como en implementación actual, sin alterar ninguno de los layouts.

La omisión anterior se debió a una extracción incompleta de estilos, una revisión de nesting insuficiente y documentación no sincronizada tras recuperar archivos. El build no detecta referencias CSS Modules sin regla emitida ni selectores Sass válidos pero no utilizados. Las precisiones técnicas de prevención se mantienen en `conventions.md`, sin herramientas nuevas ni duplicarlas en la Constitución. TD-021 continúa pendiente.

Verificación de esta corrección: `npm run build` satisfactorio (TypeScript y Sass/Vite), con la advertencia de tamaño de bundle superior a 500 kB. La lectura del CSS generado confirma descendientes del modificador con las mismas clases exportadas al JavaScript y `padding-bottom: 40px`, bajo viewport de al menos 1440 × 880 px. También se emite la clase raíz de Content con sus declaraciones restauradas. No se ejecutaron otros controles: quedan pendientes la revisión visual responsive, la altura efectiva del footer y la ausencia de superposición al desplazar contenido. El build no acredita estas comprobaciones.

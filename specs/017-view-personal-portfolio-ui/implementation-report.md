# Entrega técnica — SPEC-017

Fecha: 2026-10-21. Rama: `feat/017-view-personal-portfolio-ui`.
Alcance autorizado: T001–T039; prohibidos builds, tests y escrituras de historial Git.

## Resultado y límites de evidencia

37 tareas marcadas: T001–T036 y T039. T037 (build) y T038 (escenarios manuales) pendientes del
usuario. Revisión por lectura y diff; no se afirma compilación, funcionamiento observado ni
fidelidad visual verificada. No hay tests nuevos ni infraestructura de pruebas. No se modificó backend.
La especificación conserva su estado de preparación; esta entrega no acredita aceptación ni cierre.

## Code Review Report

### Funcionamiento y orden de revisión recomendado

1. **Contrato y transformación**: `frontend/src/features/portfolio/types/{dtos,models,index}.ts`,
   `portfolio.mapper.ts` y `portfolio.service.ts`. DTO/Model separados; asignaciones 1:1;
   `http.get<PortfolioResponseDTO>`, autenticación y AbortSignal del cliente existente.
   No hay type guards, recálculos, ordenamiento ni validación runtime de datos HTTP.
2. **Estado remoto**: `constants/{portfolioQueryKeys,portfolioLabels,index}.ts`,
   `hooks/queries/{usePortfolioSummaryQuery,usePortfolioHoldingsQuery,index}.ts` y
   `hooks/pages/{usePortfolioSummaryPage,usePortfolioHoldingsPage,index}.ts`, `hooks/index.ts`.
   Resumen `size=1`, tenencias `size=6`; Todas omite status. Página/filtro locales, reinicio de
   página al cambiar filtro y corrección de página fuera de rango. El detalle deriva por ID de la
   caché, sin copiar posiciones a un store ni consultar por jugador; error oculta datos financieros.
3. **Presentación**: `utils/{formatCredits,formatPercentage,formatDateTime,index}.ts`.
   Solo formato: centésimas a créditos y puntos porcentuales sin calcular resultados. Nulos no son ceros.
4. **Componentes**: bajo `components/`, cada módulo tiene TSX, SCSS Module e `index.ts`:
   `PortfolioMetricCard`, `PortfolioValueSection`, `PortfolioPortrait`, `PortfolioPositionCard`,
   `PortfolioStatusFilter` y `PortfolioPositionDetail`, agregados por `components/index.ts`.
   Resumen completo, identidad nulable, foto con fallback, filtros accesibles, panel con cinco
   secciones y todos los campos de posición. El diálogo nativo aporta modalidad y el efecto restaura
   foco y scroll. Panel completo en móvil y cuerpo desplazable; sin cambio de URL ni solicitud extra.
5. **Páginas y composición**: módulos `pages/PortfolioSummaryPage/` y `pages/PortfolioHoldingsPage/`
   con TSX, SCSS e índices; `pages/index.ts` y `features/portfolio/index.ts` exponen solo Pages.
   Reutilizan PageLayout, PaginatedPageLayout y Button. Grilla 3/2/1 columnas y hasta seis tarjetas.
   `app/router/AppRouter.tsx` y `app/navigation/sections.ts` registran rutas privadas y pestañas.
   A1: la navegación desde resumen se incorporó con US2; historial y gráficos siguen deshabilitados.
6. **Impacto compartido**: `shared/assets/Player Generic-Icon.png` reubicado sin duplicación.
   Solo cambian imports de `features/players/components/{PlayerCard,PlayerPortrait}/*.tsx`.
   Cinco primitivos en `styles/abstracts/tokens/_colors.scss` y roles correspondientes en
   `styles/abstracts/themes/_default.scss`, con valores y contrastes del diseño aprobado.
7. **Documentación**: `docs/frontend/architecture.md` añade rutas/composición/modalidad;
   `docs/frontend/technologies.md` refleja la ubicación compartida del recurso. `data-model.md`
   precisa `openedAt` no nulo conforme al DTO real. `tasks.md` registra avance y verificaciones pendientes.

### Decisiones y motivos

- No se reutilizan internos de players: se respeta aislamiento de features, compartiendo solo el asset.
- No se agrega librería modal: `dialog` nativo satisface las interacciones del plan en navegadores modernos.
  La apertura se condiciona a `!dialog.open` porque la app usa `React.StrictMode` y el efecto se monta dos veces.
- La selección del detalle es un ID local; la información sigue bajo TanStack Query.
- Errores se presentan con helpers HTTP existentes; no se inventa una respuesta HTTP ni regla financiera.
- Superficies y roles aprobados; ningún hexadecimal se escribe en SCSS de la feature.
- Los importes usan la forma larga («créditos») en los indicadores del resumen y la compacta («cr.») en
  tarjetas y panel, siguiendo la referencia visual.
- El estado de la posición se distingue por relleno además de por texto, no solo por color.
- Contrastes verificados sobre los valores realmente declarados: positivo 8,40:1, negativo 5,29:1,
  advertencia 8,76:1 y error 7,74:1 sobre la tarjeta; mayores sobre fondo y panel. El borde de
  superficie cumple 3,07:1 sobre la tarjeta y 3,52:1 sobre el fondo.
- `openedAt` se modela como no nulo, conforme al DTO HTTP real; nombre, equipo y posición admiten nulos.

### Fidelidad visual frente a las referencias

Las dos pantallas se reconstruyeron tomando `referencias-imagenes/01-mi-portfolio.svg` y
`02-mis-tenencias.svg` como fuente de verdad visual. La comparación se hizo con bancos HTML
renderizados a 1635 px en el navegador, fuera del repositorio, sin compilar el proyecto.

**Reproducido**: panel contenedor con borde y radio 13, retícula de cuatro indicadores de 134 px con
mosaico de icono, color propio por indicador y valor de 25 px; cuatro paneles de 766 px —tenencias
con barra de distribución de tokens, resultados financieros, actividad y llamada a la acción con
botón de 295×39—; ficha de posición de 513×270 con degradado, avatar de 65 px, etiqueta de estado
con borde, seis métricas en tres columnas con separadores, resultado y enlace de detalle; filtros
*Todas* / *Abiertas* / *Cerradas* con la pastilla de la página activa y la cuenta de posiciones;
pie con el total y la paginación. La paleta de la referencia quedó registrada como primitivos y
roles semánticos de Cartera: ningún componente escribe hexadecimales.

**Diferencias que persisten, con su causa**:

| Diferencia | Causa |
| --- | --- |
| «Posiciones abiertas» en lugar de «Posiciones» | DC-09 exige rótulo explícito para no confundirlo con el total de la lista |
| Sin insignia «Datos de ejemplo» ni «Datos ficticios» | Artefactos de maqueta; los datos son reales del backend |
| Avatar genérico de la aplicación en lugar de la silueta de la maqueta | DC-06 conserva el ícono genérico ya establecido en el proyecto |
| Iconos de métrica semánticos de lucide | La maqueta reutiliza un glifo genérico para dos métricas distintas |
| Paginación como cápsula compartida, no como botones alineados a la derecha | `shared/components/Pagination` no puede modificarse sin afectar al catálogo de jugadores |
| Origen vertical del contenido | El navbar y la barra de pestañas son componentes compartidos: su altura la fija la aplicación, no esta feature |

### Checklist humano de revisión — no aprobado por el agente

- [ ] Revisar contrato tipado, nulabilidad, Mapper y ausencia de cálculos financieros.
- [ ] Revisar privacidad, cancelación y manejo de errores con la infraestructura existente.
- [ ] Revisar filtros, paginación, orden recibido y resumen global independiente de la página.
- [ ] Revisar las dos rutas, A1 y exclusión de historial/gráficos/operaciones de mercado.
- [ ] Revisar foco modal, Escape, retorno al botón, scroll, responsive y contraste en navegador.
- [ ] Revisar imports del asset reubicado y documentación transversal.
- [ ] Revisar la exención de tests: no se crearon tests ni infraestructura; no se ejecutaron pruebas.

## Verificación pendiente del usuario

1. Ejecutar `npm run build` desde `frontend/` y aportar el resultado (T037).
2. Con backend y sesiones apropiadas, recorrer `quickstart.md`, registrar escenarios ejecutados y
   omisiones por falta de datos (T038). Revisar especialmente errores 401/503, nombres nulos,
   cotización ausente frente a posición cerrada, último grupo paginado y teclado en móvil.
3. Si el build o la comprobación visual muestran defectos, corregir antes de aceptación humana.

No se declara CODE REVIEW aprobado, TESTING aprobado ni Feature completada.

## Actualización visual posterior — referencia PNG

El encargo posterior toma `referencias-imagenes/01-mi-portfolio.png` (1672 × 941, interfaz web sin marco de sistema) como fuente visual principal del resumen. No sustituye la evidencia histórica anterior ni acredita aceptación de la feature.

### Composición implementada

- Fondo marino profundo con iluminación sutil; el wrapper del resumen conserva su función estructural, pero pierde su superficie y borde envolventes.
- Encabezado con icono de estadísticas y tarjeta informativa no interactiva. Dice «Datos de tu portfolio», no «Datos de ejemplo»: los datos siguen siendo reales.
- Cuatro métricas con rail lateral, mosaico grande de icono, gradientes, tipografía destacada, indicador decorativo y acento inferior; el resultado negativo conserva el signo y usa acento negativo.
- Paneles con iconos por fila, separadores, distribución verde/violeta con cantidades y porcentajes reales, valoración actual lateral, operaciones registradas en tarjeta violeta y llamada a la acción verde hacia `/portfolio/holdings`.
- La valoración reutiliza `totalCurrentValue` y `totalProfitLossPercentage`. La barra representa solo la magnitud visual del porcentaje existente, acotada a su ancho: no recalcula rentabilidad ni implica otra métrica; cero no se reemplaza por el relleno ilustrativo de la referencia. Los nulos conservan el formato existente «—».

### Contención del impacto

- `PortfolioValueSection` tiene variante opcional `dashboard`, utilizada únicamente por el resumen. Sus valores por defecto y los de Header/Rows/Row conservan la apariencia del detalle de tenencias.
- Navbar usa una variante `financial` explícita desde AppLayout **solo en `/portfolio`**. Los iconos y colores de badges son metadatos presentacionales opcionales; no se cambian rutas, estado activo, autenticación ni opciones deshabilitadas.
- La nueva paleta `finance` se añade mediante primitivos y roles semánticos, sin sustituir tokens usados por otras páginas. Se reutilizan espaciados, breakpoints, textos y colores semánticos existentes.
- Se mantienen las columnas fluidas: métricas 4/2/1, paneles 2/1, interiores apilados en móvil y scroll de pestañas. En el resumen, AppLayout acompaña el breakpoint móvil de PageLayout para evitar recortes entre 701 y 768 px. No se modifica PageLayout, su footer flotante ni la banda de 40 px.
- No se modifican DTO, Models, Services, Hooks ni cálculos financieros. Los campos nuevos de `types/ui.ts` son únicamente iconos y énfasis de presentación.

### Verificaciones y pendientes

Se inspeccionaron la imagen y la captura actual aportadas, los componentes, consumidores y tokens. `npm run build` compila TypeScript y Sass/Vite; la lectura del CSS emitido permite revisar los selectores de las variantes. No se ejecutan Biome, tests, el comprobador SCSS ni sesiones de navegador: el control autorizado por `frontend/AGENTS.md` es únicamente el build.

Resultado observado del build de esta actualización: satisfactorio; persiste la advertencia de bundle JavaScript mayor de 500 kB. No se declara comparación visual de la aplicación renderizada.

**Pendiente de comprobación visual humana:** comparación de `/portfolio` autenticado con la PNG a 1672 × 941 de área web (sin barras del navegador), adaptación a tablet/móvil, importes largos, resultados negativos, nulos, estados vacío/carga/error y navegación por teclado. Comparar además `/players`, `/players/:id` y `/portfolio/holdings`, que conservan las variantes por defecto. El build y la revisión de selectores no demuestran fidelidad de píxeles ni ausencia de regresiones visuales. TD-021 no se modifica ni se declara resuelta.

### Corrección posterior del presupuesto vertical del resumen

**Problema reportado:** scroll interno de desktop y última fila parcialmente fuera del viewport. La implementación visual sumó mínimos y espacios independientes sin repartir la altura restante entre el wrapper, Content y sus filas. El build anterior no medía ese presupuesto ni constituía validación visual.

**Diagnóstico por lectura:** AppLayout ya limita el conjunto a `100dvh` y descuenta la navegación mediante flex; PageLayout ya contiene `min-height: 0` y el único scroll de desktop. La navegación financiera consume sus bandas de 80 y 64 px, más bordes. El resumen añadía padding vertical de PageLayout y de su wrapper (16 + 34 px), encabezado de al menos 76 px, margen de 24 px, dos gaps de 18 px, métricas de al menos 152 px y la altura intrínseca de ambos grupos de paneles. La valoración y el recuadro de actividad también suman padding y líneas propias. La suma, sensible a saltos de línea e importes, puede exceder el espacio disponible aunque cada bloque aislado parezca respetar la referencia. No interviene un footer: esta Page no lo configura y no activa `floatingFooter`.

**Solución localizada, sin recortes:**

- El wrapper y Content forman una cadena flex vertical que utiliza la altura restante. Los grupos de paneles reciben el espacio sobrante según su tamaño natural; las métricas no se estiran para rellenarlo. Se conserva `flex-shrink: 0` en los bloques de contenido que deben mantener su altura intrínseca: si no caben, el scroll existente permite acceder a ellos.
- En el modo compacto existente (ancho mínimo 1440 px y alto mínimo 880 px), el padding vertical del wrapper baja a un escalón de 8 px, evitando acumularlo con el padding de PageLayout.
- Solo bajo ese modo, variables CSS locales al resumen ajustan gradualmente los espacios según la diferencia respecto de los 941 px de área web de la PNG. La presión se acota entre cero y 4 px: reduce gaps, márgenes y padding, nunca tipografías. El mínimo de métricas puede pasar de 152 a 136 px; el padding superior y la separación del valor acompañan esa reducción para que el contenido no fuerce el mínimo anterior. El encabezado ajusta también el margen del subtítulo y el padding informativo, no solo su `min-height`.
- Las tarjetas de valoración y actividad reducen su padding de 16 a un mínimo de 12 px mediante la misma escala. No se fija una altura máxima a sus datos, filas o textos.
- Por debajo de los umbrales, se conservan las dimensiones naturales, el apilamiento y el scroll necesario. Los valores largos o el zoom pueden requerir scroll incluso en desktop; no se ocultan ni se comprimen los datos para impedirlo.

**Prevención:** mantener el presupuesto conjunto navegación → encabezado → métricas → filas al ajustar espacios del dashboard. Los mínimos de tarjeta no bastan: también deben revisarse sus contenidos y padding. Esta decisión pertenece a los módulos locales del resumen; no crea reglas globales de overflow ni modifica Navbar, AppLayout, PageLayout, sus breakpoints o la banda flotante de 40 px.

**Evidencia:** `npm run build` satisfactorio, con la advertencia ya existente de bundle mayor de 500 kB. Lectura del CSS emitido para comprobar la cadena flex y las variables de densidad bajo la media query compacta. Sin tests, Biome, comprobador SCSS ni navegador. **Pendiente:** comprobar visualmente ausencia de scroll y última fila completa a 1672 × 941; repetir en desktop de 900/880 px de alto, con menor altura, zoom, datos largos y tablet/móvil. No se declara acreditado el criterio visual de aceptación por el build.

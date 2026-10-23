# Guía de validación: Visualizar portfolio personal (SPEC-017)

**Feature**: SPEC-017 · **Rama**: `feat/017-view-personal-portfolio-ui` · **Fecha**: 2026-10-21
**Plan**: [`../plan.md`](../plan.md) · **Spec**: [`../spec.md`](../spec.md) ·
**Contrato**: [`contracts/portfolio-consumption.md`](contracts/portfolio-consumption.md)

Guía de comprobación manual de la sección Cartera. El frontend está exento de testing de forma temporal
(Constitution, *Verificación y testing*; `frontend/AGENTS.md`), así que **esta feature no crea tests ni
infraestructura de testing**: la validación se apoya en el control ejecutable permitido y en los
escenarios de esta guía.

Los escenarios no reproducen implementación: describen qué se observa en la interfaz y qué se espera.

---

## 1. Prerrequisitos

- Backend accesible con la consulta `GET /api/portfolio/me` implementada, como entrega SPEC-014.
- Un usuario autenticado con posiciones, con al menos una posición **abierta con cotización**, al menos
  una **abierta sin cotización** y al menos una **cerrada**. Si no se dispone de ese conjunto, los
  escenarios 3.4 y 3.5 se omiten y se registran como no ejecutados.
- Un usuario autenticado **sin posiciones**, para el escenario 3.2.

## 2. Comandos de validación

```bash
# Control ejecutable permitido para el frontend. Lo ejecuta el usuario.
cd frontend && npm run build
```

El build ejecuta el type-check de TypeScript en modo estricto y el build de Vite. Un fallo de
compilación es un fallo del cambio.

Para la comprobación manual, la aplicación se levanta con el comando de desarrollo habitual del
frontend y se recorre con la sesión autenticada.

## 3. Escenarios de validación

### 3.1 Acceso y navegación — historia 1

| # | Given | When | Then |
| --- | --- | --- | --- |
| 1 | Usuario autenticado | Entera a la sección Cartera | Se abre la pestaña *Mi portfolio*, marcada como activa, y el navbar marca Cartera como sección activa |
| 2 | Usuario autenticado en *Mi portfolio* | Mira la pantalla | Se presentan los trece indicadores del resumen: valor actual, capital invertido, resultado total con su rentabilidad, posiciones abiertas, total de tokens, tokens disponibles, tokens reservados, compras realizadas, ventas realizadas, resultado realizado, resultado no realizado y estado de la valoración |
| 3 | Usuario autenticado en *Mi portfolio* | Compara el indicador de posiciones con el total listado en *Mis tenencias* | El indicador de *Mi portfolio* está rotulado como posiciones **abiertas** y no se confunde con el total de la lista (DC-09) |
| 4 | Usuario autenticado en *Mi portfolio* | Pulsa la acción que lleva a *Mis tenencias* | Se abre la pestaña *Mis tenencias* con el filtro *Todas* seleccionado |
| 5 | Usuario autenticado | Recorre el navbar | Las secciones y pestañas previas no cambian de lugar ni de aspecto; las pestañas *Historial de transacciones* y *Estadísticas/gráficos* siguen deshabilitadas y rotuladas como pendientes |

### 3.2 Estados de la interfaz — historias 1 y 2

| # | Given | When | Then |
| --- | --- | --- | --- |
| 6 | Usuario **sin posiciones** | Abre *Mi portfolio* | Se presenta un estado vacío comprensible y los indicadores numéricos en cero; no se simulan posiciones |
| 7 | Usuario **sin posiciones** | Abre *Mis tenencias* | Se presenta un estado vacío comprensible, distinto del vacío de un filtro sin coincidencias |
| 8 | Consulta en curso | Se observa la vista | Se anuncia el estado de carga y el contenedor de la página queda marcado como ocupado |
| 9 | La consulta falla | Se observa la vista | Se presenta el mensaje del error y una acción de reintento; **no** se muestra ningún importe financiero |

### 3.3 Filtro y paginación — historia 2

| # | Given | When | Then |
| --- | --- | --- | --- |
| 10 | Usuario con posiciones abiertas y cerradas | Selecciona *Abiertas* | Solo se muestran posiciones abiertas; el filtro se marca como activo |
| 11 | Usuario con posiciones abiertas y cerradas | Selecciona *Cerradas* | Solo se muestran posiciones cerradas |
| 12 | Usuario con posiciones abiertas y cerradas | Vuelve a *Todas* | Se muestran posiciones abiertas y cerradas |
| 13 | Usuario con más de seis posiciones | Cambia de página | Se muestra el siguiente grupo de posiciones, la página actual queda identificada y el filtro se conserva |
| 14 | Usuario con posiciones | Aplica un filtro | La vista vuelve a la primera página del conjunto filtrado |
| 15 | Filtro sin posiciones | Observa la vista | Se presenta un estado vacío del conjunto filtrado, distinto del vacío global |
| 16 | Conjunto con una sola página | Observa la vista | Los controles de paginación están presentes y reflejan que solo hay una página |

### 3.4 Tarjeta de posición — historia 2

| # | Given | When | Then |
| --- | --- | --- | --- |
| 17 | Posición con imagen | Se muestra su tarjeta | Aparece el retrato del jugador y el nombre permanece visible |
| 18 | Posición **sin imagen** | Se muestra su tarjeta | Aparece el avatar predeterminado, sin perder el nombre ni ningún dato financiero |
| 19 | Posición **abierta con cotización** | Se muestra su tarjeta | Se presentan tokens totales, disponibles y reservados, precio promedio, precio actual, valor actual y resultado con su signo |
| 20 | Posición **abierta sin cotización** | Se muestra su tarjeta | El precio actual muestra *Sin cotización*, el valor actual y el resultado muestran guion, y el precio promedio y la cantidad siguen visibles |
| 21 | Posición **cerrada** | Se muestra su tarjeta | El precio medio, el precio actual y el valor actual muestran guion, y el resultado corresponde al realizado acumulado |
| 22 | Posición con resultado negativo | Se muestra su tarjeta | El importe conserva su signo y se distingue visualmente del resultado positivo y de la ausencia de dato |
| 22b | Conjunto de posiciones | Se contrasta el orden de las tarjetas con el de la respuesta del backend | Las tarjetas se presentan en el mismo orden que devuelve la consulta, sin reordenamiento por valor, por nombre ni por ningún otro criterio |
| 22c | Posición con `playerName`, `teamName` o `position` nulos | Se consulta la tarjeta y su panel | Cada dato ausente se comunica sin inventarlo; se conservan la posición y sus cifras, y el retrato tiene texto alternativo genérico si no existe nombre |

### 3.5 Panel de detalle — historia 3

| # | Given | When | Then |
| --- | --- | --- | --- |
| 23 | Usuario en la grilla | Pulsa *Ver detalles* en una tarjeta | Se superpone un panel lateral con el detalle de esa posición; la grilla permanece visible detrás |
| 24 | Panel abierto | Se recorre su contenido | Se presentan las cinco secciones: identificación, tenencia, inversión y valoración, actividad y resultados |
| 25 | Panel abierto | Se observa la red | No se realiza ninguna consulta adicional: el detalle se construye con los datos ya cargados |
| 26 | Panel abierto | Se pulsa *Escape* o el control de cierre | El panel se cierra y la grilla vuelve a su estado anterior, con el mismo filtro y la misma página |
| 27 | Panel abierto | Se navega con el teclado | El foco queda dentro del panel y no alcanza el contenido de fondo; al cerrar, el foco vuelve a la acción que lo abrió |
| 28 | Panel abierto | Se cambia de tamaño la ventana | El panel permanece accesible y su contenido es legible |
| 28b | Panel abierto en una ventana de 360 px | Se observa el panel | El panel ocupa todo el ancho del área de contenido; su cabecera con el control de cierre permanece visible, el cuerpo se desplaza dentro del panel y el documento no se desplaza |
| 28c | Panel abierto en una ventana de 360 px | Se navega con el teclado y se pulsa `Escape` | El foco sigue confinado en el panel, `Escape` lo cierra y el foco vuelve a la acción que lo abrió; el contenido de la pestaña sigue siendo inalcanzable mientras el panel está abierto |

### 3.6 Coherencia con el contrato

| # | Given | When | Then |
| --- | --- | --- | --- |
| 29 | Cualquier vista | Se contrasta lo mostrado con la respuesta de `GET /api/portfolio/me` para la misma petición | Cada importe visible es el valor informado expresado en créditos con dos decimales; cada porcentaje es el informado con dos decimales; ningún valor difiere |
| 30 | Indicador que el backend no informa | Se observa la vista | Se muestra un guion, nunca un cero |
| 31 | Posición abierta sin cotización | Se observa el resumen | El resumen se presenta con los indicadores disponibles y el estado de valoración incompleto |
| 32 | Cualquier vista | Se inspecciona la red | La vista realiza una sola solicitud al backend y no realiza ninguna por posición |

### 3.7 Responsive y estilo

| # | Given | When | Then |
| --- | --- | --- | --- |
| 33 | Ventana de 1440 px | Se abre *Mis tenencias* | La retícula presenta tres columnas por dos filas con seis tarjetas |
| 34 | Ventana de 768 px | Se observa *Mis tenencias* | La retícula se readapta a dos columnas y el tamaño de página sigue siendo de seis tarjetas |
| 35 | Ventana de 360 px | Se observa *Mis tenencias* | La retícula se readapta a una columna; ninguna tarjeta queda cortada ni ninguna etiqueta truncada |
| 36 | Cualquier vista | Se compara con el catálogo de jugadores | El navbar, el tema y el tratamiento tipográfico son coherentes con el resto de la aplicación |
| 37 | Cualquier vista | Se lee el texto de la interfaz | El texto usa tuteo, como el resto de la aplicación (DC-10), y no contiene las menciones a datos de ejemplo de la maqueta |

### 3.8 Límites de alcance

| # | Given | When | Then |
| --- | --- | --- | --- |
| 38 | Cualquier vista de la sección Cartera | Se recorren todas sus acciones | Ninguna permite comprar, vender ni publicar tokens, y ninguna ofrece un historial de operaciones, un gráfico ni un panel de estadísticas: las pestañas *Historial de transacciones* y *Estadísticas/gráficos* siguen deshabilitadas |
| 39 | Cualquier vista de la sección Cartera | Se inspecciona la red | No se emite ninguna solicitud de escritura —ningún método distinto de lectura— y no se presenta ningún dato de demostración |

## 4. Resultado esperado

- El build del frontend finaliza sin errores.
- Los escenarios ejecutados se cumplen en su totalidad.
- Los escenarios omitidos por falta de datos se registran como **no ejecutados**, con el motivo, y no se
  dan por cumplidos.

## 5. Referencias

- Especificación funcional: [`spec.md`](spec.md)
- Plan: [`plan.md`](plan.md)
- Modelo de datos: [`data-model.md`](data-model.md)
- Contrato de consumo: [`contracts/portfolio-consumption.md`](contracts/portfolio-consumption.md)
- Contrato HTTP de origen: `specs/014-view-personal-portfolio/contracts/portfolio-api.md`
- Referencias visuales: `referencias-imagenes/01-mi-portfolio.svg`,
  `referencias-imagenes/02-mis-tenencias.svg` y
  `referencias-imagenes/03-mis-tenencias-detalle-abierto.svg`

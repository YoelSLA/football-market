# Implementation Plan: Visualizar portfolio personal (SPEC-017)

<!-- MUST usar rama real según Constitution §7 y seguir docs/development/sdd-workflow.md.
Plan no genera Tasks ni habilita Implement: siguen Checklists, Tasks y Analyze.
Complexity Tracking registra decisiones, no autoriza violaciones de Constitution. -->

**Branch**: `feat/017-view-personal-portfolio-ui` | **Date**: 2026-10-21 | **Spec**: [`spec.md`](spec.md)

**Input**: Feature specification from `specs/017-view-personal-portfolio-ui/spec.md`

## Summary

SPEC-017 habilita la sección **Cartera** del frontend con dos pestañas navegables —*Mi portfolio* y
*Mis tenencias*— que consumen `GET /api/portfolio/me`, la consulta del portfolio propio ya
implementada por SPEC-014.

El diseño crea una feature nueva `features/portfolio/` que sigue la arquitectura vigente: Page → Hook
→ Service, DTO y Model separados con un Mapper de transformación tipada, estado remoto en
TanStack Query y estilos SCSS Modules que consumen roles semánticos. No se recalcula ninguna regla
financiera: la interfaz presenta los indicadores y los porcentajes tal como el backend los informa,
distinguiendo la ausencia de dato del valor cero.

Dos elementos del diseño superan el alcance de la feature y se justifican en §16: la reubicación del
ícono genérico de jugador a `shared/` —porque una feature no puede depender de otra— y el nacimiento
de cinco roles semánticos sobre superficie oscura —tres de estado financiero, uno de error y uno de
borde—, que hoy no existen y son
imprescindibles para que los resultados sean legibles sobre las tarjetas oscuras de la referencia
aprobada.

## Technical Context

**Language/Version**: TypeScript 7.0.2 en modo estricto; React 19.3.0; Node 24.19.0 (`.nvmrc`)

**Primary Dependencies**: React 19.3.0 · React Router DOM 7.18.4 · TanStack Query 5.103.1 ·
Axios 1.20.0 · Sass 1.104.1 · Lucide React 1.59.0 · Vite 8.3.0 · Biome 2.5.14. **Ninguna dependencia
nueva**: la feature reutiliza el stack vigente.

**Storage**: No aplica. No hay estado remoto propio ni persistencia de la feature; la sesión vive en
`infrastructure/storage` y el estado remoto en la caché de TanStack Query.

**Testing**: **No aplica.** El frontend está exento de testing de forma temporal (Constitution,
*Verificación y testing*; `frontend/AGENTS.md`: «Por el momento no crear tests de frontend ni
incorporar infraestructura para ellos»). La validación de esta feature es el control ejecutable
permitido (`npm run build` desde `frontend/`) más la comprobación manual guiada por
[`quickstart.md`](quickstart.md). No se crea infraestructura de testing.

**Target Platform**: Navegador web moderno, escritorio y móvil, de 360 a 1440 px de ancho.

**Project Type**: Frontend web (SPA) sobre el backend Spring Boot existente.

**Performance Goals**: Una solicitud de red por vista, sin peticiones por posición ni por jugador.
La consulta del resumen se resuelve con el tamaño de página mínimo admitido por el contrato, porque
el resumen es independiente del filtro y de la página. La caché de TanStack Query reutiliza su
`staleTime` global de 30 s, de modo que alternar entre las dos pestañas dentro de ese intervalo no
genera tráfico adicional.

**Constraints**: Sin dependencias nuevas (Constitution §I). Sin librería de gráficos ni de componentes
(la sección no los necesita). Los componentes no escriben valores cromáticos literales: consumen roles
semánticos de `styles/abstracts/themes/_default.scss`. Una feature no puede depender de otra.

**Scale/Scope**: 2 pantallas, 10 componentes de feature, 1 service, 1 mapper, 2 query hooks,
2 page hooks, 2 rutas nuevas, 1 reubicación de recurso y 3 roles semánticos nuevos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Verificación | Resultado |
| --- | --- | --- |
| §I Simplicidad y calidad | Cero dependencias nuevas. Cada componente extraído tiene reutilización real: `PortfolioValueSection` se usa en 7 bloques (2 del resumen, 5 del panel). `PortfolioMetricCard` se usa 4 veces. No se crean abstracciones hipotéticas: no hay capa de «tarjeta base», ni store de feature, ni capa de presentación reutilizable genérica. | ✅ |
| §II Proporcionalidad del alcance | El alcance es exactamente el de `spec.md`: 2 pestañas, 6 tarjetas por página, filtros, panel lateral. No se agrega modo de demostración, ni navegación a la ficha del jugador, ni gráficos, ni historial. | ✅ |
| §III Seguridad e integridad | La interfaz nunca envía un identificador de usuario: la identidad la resuelve el backend desde la sesión. No se interpretan tokens ni se guarda información financiera en `localStorage`. No se registran secretos. | ✅ |
| §IV Coherencia de contratos | Se consume el contrato de SPEC-014 sin redefinirlo ni duplicarlo. La reubicación del ícono genérico exige actualizar el inventario de recursos de `docs/frontend/technologies.md` en el mismo cambio. Los roles nuevos se documentan en el propio archivo de tokens. | ✅ (condición: actualizar `technologies.md`) |
| §V Trazabilidad | `SPEC-017` ↔ US1-US3 ↔ `tasks.md` ↔ rama `feat/017-view-personal-portfolio-ui`. El comportamiento de la interfaz no se deduce de la tarjeta de seguimiento. | ✅ |
| Verificación y testing | El agente no ejecuta tests. El frontend no crea tests. La verificación es `npm run build` y la comprobación manual de `quickstart.md`. | ✅ |
| Idioma | Código e identificadores en inglés; documentación, mensajes de interfaz y comentarios en español. El texto de interfaz usa tuteo (DC-10). | ✅ |
| Autorizaciones humanas | Este plan no habilita implementación alguna. Requiere Checklists, Tasks, Analyze y autorización humana explícita. | ✅ |

**Complexity Tracking**: sin violaciones. No se requiere tabla de excepciones.

## Project Structure

### Documentation (this feature)

```text
specs/017-view-personal-portfolio-ui/
├── plan.md              # Este archivo (/speckit.plan)
├── spec.md              # Especificación funcional (Specify + Clarify)
├── research.md          # Fase 0 (/speckit.plan)
├── data-model.md        # Fase 1 (/speckit.plan)
├── quickstart.md        # Fase 1 (/speckit.plan)
├── contracts/
│   └── portfolio-consumption.md   # Fase 1: contrato de consumo del frontend
├── checklists/
│   └── requirements.md # Checklist de calidad de la especificación
└── tasks.md             # Fase 2 (/speckit.tasks) — NO creado por /speckit.plan
```

### Source Code (repository root)

```text
frontend/src/
├── app/
│   ├── navigation/
│   │   └── sections.ts                     # MODIFICADO: habilita Cartera y sus 2 pestañas
│   └── router/
│       └── AppRouter.tsx                   # MODIFICADO: registra /portfolio y /portfolio/holdings
├── shared/
│   ├── assets/
│   │   └── Player Generic-Icon.png         # REUBICADO desde features/players/assets/
│   └── components/                         # sin cambios
├── styles/abstracts/
│   ├── tokens/_colors.scss                 # MODIFICADO: 3 primitivos de estado sobre superficie oscura
│   └── themes/_default.scss                # MODIFICADO: 3 roles semánticos de estado financiero
└── features/
    ├── players/                            # MODIFICADO: 2 imports del ícono genérico
    └── portfolio/                          # NUEVA
        ├── portfolio.service.ts            # GET /api/portfolio/me
        ├── portfolio.mapper.ts             # Transformación tipada DTO → Model
        ├── constants/
        │   ├── index.ts
        │   ├── portfolioQueryKeys.ts
        │   └── portfolioLabels.ts          # rótulos, textos y filtros
        ├── types/
        │   ├── index.ts
        │   ├── dtos.ts
        │   ├── models.ts
        │   └── ui.ts                      # tipo de UI compartido de la feature
        ├── utils/
        │   ├── index.ts
        │   ├── formatCredits.ts
        │   ├── formatPercentage.ts
        │   └── formatDateTime.ts
        ├── hooks/
        │   ├── index.ts
        │   ├── queries/
        │   │   ├── usePortfolioSummaryQuery.ts
        │   │   └── usePortfolioHoldingsQuery.ts
        │   └── pages/
        │       ├── usePortfolioSummaryPage.ts
        │       └── usePortfolioHoldingsPage.ts
        ├── components/
        │   ├── index.ts
        │   ├── PortfolioMetricCard/
        │   ├── PortfolioValueSection/
        │   ├── PortfolioValueHeader/
        │   ├── PortfolioValueRows/
        │   ├── PortfolioValueRow/
        │   ├── PortfolioTokenDistribution/
        │   ├── PortfolioPortrait/
        │   ├── PortfolioPositionCard/
        │   ├── PortfolioStatusFilter/
        │   └── PortfolioPositionDetail/
        ├── pages/
        │   ├── index.ts
        │   ├── PortfolioSummaryPage/
        │   └── PortfolioHoldingsPage/
        └── index.ts                        # API pública de la feature
```

## Diseño técnico

### 1. Ubicación de la feature y límites de dependencia

La feature vive en `frontend/src/features/portfolio/`. Su API pública expone únicamente las dos Pages
que `app/router` necesita:

```ts
export { PortfolioHoldingsPage, PortfolioSummaryPage } from "./pages";
```

No exporta componentes, hooks ni modelos: `app` solo necesita componer rutas, y la navegación de la
sección se habilita desde `app/navigation/sections.ts` con las rutas ya registradas por el router.

La feature **no depende** de `features/players`. El único recurso compartido que necesita —el ícono
genérico de jugador— se reubica en `shared/` (§11).

### 2. Integración HTTP

El Service es la única frontera HTTP de la feature y es el dueño del endpoint:

```ts
// features/portfolio/portfolio.service.ts
async function getPortfolio(
	params: PortfolioQueryParams,
	signal: AbortSignal,
): Promise<Portfolio> {
	const { data } = await http.get<PortfolioResponseDTO>("/portfolio/me", {
		authenticated: true,
		params,
		signal,
	});

	return toPortfolio(data);
}
```

El Service sigue el patrón de `auth.service.ts`: respuesta DTO tipada y transformación a Model
dentro de su frontera. Conforme a arquitectura frontend §1.8, confía en el contrato tipado, sin
validaciones runtime ad hoc. El genérico de Axios no constituye validación runtime.

**Parámetros de la consulta** (`PortfolioQueryParams`):

| Parámetro | Valor en la vista de resumen | Valor en la vista de tenencias |
| --- | --- | --- |
| `page` | `0` | página seleccionada, desde cero |
| `size` | `1` — mínimo admitido por el contrato, porque el resumen no depende del tamaño | `6` — el tamaño de página aprobado |
| `status` | ausente | `OPEN`, `CLOSED` o ausente para *Todas* |

El resumen se pide con `size=1` porque el contrato garantiza que representa el portfolio completo con
independencia del filtro y de la página: traer seis posiciones que la vista de resumen no muestra solo
encarece el payload.

### 3. Contrato, DTO y Model

El contrato autoritativo es `specs/014-view-personal-portfolio/contracts/portfolio-api.md`. Esta
feature **no lo reescribe**: el DTO de `types/dtos.ts` refleja sus campos y el Mapper los transforma.
El detalle campo por campo está en [`data-model.md`](data-model.md) y en
[`contracts/portfolio-consumption.md`](contracts/portfolio-consumption.md).

La separación DTO ↔ Model se mantiene aunque several campos coincidan, porque la arquitectura lo exige
de forma expresa (§1.8). El Model expresa lo que la interfaz necesita:

- Los importes se modelan en **centésimas de crédito**, tal como llegan. La conversión a créditos ocurre
  en la capa de presentación (`utils/`), nunca en el Service ni en el Mapper: el Mapper no calcula.
- La ausencia de dato se modela como `null` y **nunca** como `0`. Es la distinción que sostiene
  FR-004, FR-029 y AC05.
- El estado de la posición y el estado de la valoración son uniones literales (`"OPEN" | "CLOSED"`,
  `"VALUED" | "UNVALUED" | "NOT_APPLICABLE"`), no cadenas libres.

### 4. Mapper limitado a transformación

`portfolio.mapper.ts` recibe `PortfolioResponseDTO` y devuelve `Portfolio`. Copia explícitamente
resumen, posiciones y paginación hacia tipos Model separados, conservando valores, nulos y orden.
No recibe `unknown`, no usa type guards ni revalida formas, cantidades, paginación o reglas contables.
Las garantías de coherencia pertenecen al contrato y al backend de SPEC-014/SPEC-015.
Los formateadores presentan los importes; el Mapper no calcula indicadores ni corrige respuestas.
Una ruptura del contrato exige corregir su fuente: el tipado no garantiza detectar en runtime
respuestas malformadas y este diseño no promete ese comportamiento.

### 5. Estado remoto: Query Hooks

Dos Query Hooks, porque las dos vistas piden datos con parámetros distintos y sus claves deben ser
independientes: una entrada de caché que variara con la página invalidaría el resumen al paginar.

| Query Hook | Clave | Parámetros | Vista |
| --- | --- | --- | --- |
| `usePortfolioSummaryQuery` | `["portfolio", "summary"]` | `page=0`, `size=1` | *Mi portfolio* |
| `usePortfolioHoldingsQuery` | `["portfolio", "holdings", status, page]` | `page`, `size=6`, `status?` | *Mis tenencias* |

Las claves viven en `constants/portfolioQueryKeys.ts`, como exige §1.13. El Service no depende de
TanStack Query: solo los Query Hooks lo hacen.

El `staleTime` global de 30 s cubre la alternancia entre pestañas sin tráfico adicional. La caché se
invalida sola al cerrar la sesión, porque `app/providers/SessionLifecycle` limpia la Query Cache (§1.10
y §1.11): ningún dato financiero de una identidad sobrevive a otra.

### 6. Estado de vista: Page Hooks

**`usePortfolioSummaryPage`** expone el resumen, el estado de carga, el mensaje de error y el reintento.
No coordina nada más: la vista no tiene filtros ni paginación.

**`usePortfolioHoldingsPage`** coordina:

- `page` (página seleccionada) y `status` (filtro: `ALL | OPEN | CLOSED`).
- La consulta de tenencias con esos parámetros.
- El recorte de página inválida: cuando la respuesta informa menos páginas que la seleccionada, se
  vuelve a la última página válida. Es el patrón ya establecido en `usePlayersPage`.
- El reinicio a la primera página al cambiar el filtro.
- `selectedPosition`: la posición cuyo panel de detalle está abierto. El estado vive en el Page Hook y
  no en la tarjeta, porque lo consumen la grilla y el panel: es estado compartido de la vista (§1.10).

### 7. Componentes

| Componente | Responsabilidad | Reutilización |
| --- | --- | --- |
| `PortfolioMetricCard` | Indicador destacado: rótulo, valor y texto auxiliar. | 4 usos en *Mi portfolio* |
| `PortfolioValueSection` | Panel con título, cuerpo y zona inferior. | 4 usos: los tres paneles del resumen y la llamada a la acción |
| `PortfolioValueHeader` | Cabecera de un panel: mosaico de icono y título. | 1 uso, dentro de `PortfolioValueSection` |
| `PortfolioValueRows` | Lista de filas de rótulo y valor. | 1 uso, dentro de `PortfolioValueSection` |
| `PortfolioValueRow` | Fila de rótulo y valor, con su tono de resultado. | 1 uso por fila, dentro de `PortfolioValueRows` |
| `PortfolioTokenDistribution` | Barra de distribución de tokens con su leyenda. | 1 uso, en el panel de tenencias |
| `PortfolioPortrait` | Retrato del jugador con degradación al ícono genérico. | 2 usos: la tarjeta de posición y la cabecera del panel |
| `PortfolioPositionCard` | Tarjeta de posición: retrato, identidad, tenencia, valoración, resultado y acción *Ver detalles*. | 6 usos por página |
| `PortfolioStatusFilter` | Filtro *Todas* / *Abiertas* / *Cerradas*. | 1 uso, control propio del dominio |
| `PortfolioPositionDetail` | Panel lateral con el detalle completo de la posición. | 1 uso |

La sección, su cabecera, su lista de filas y su fila se separan porque tienen responsabilidades
distintas y porque el detalle del panel reutiliza la misma forma de bloque con título y filas. El tipo
de la fila es un **tipo de UI compartido de la feature**, declarado una sola vez en `types/ui.ts`,
expuesto por el barrel de `types/` y consumido por los tres componentes. No se replica su forma en las
props de cada uno.

`PortfolioPortrait` evita duplicar la lógica de degradación de la imagen: la necesitan la tarjeta de
posición y la cabecera del panel de detalle, que muestra el mismo retrato. Vive dentro de la feature —
una feature no puede depender de otra— y consume el ícono genérico reubicado en `shared/`. Como la
consulta del portfolio solo expone `imageUrl`, la cadena es `imageUrl` → ícono genérico. No existe
`fallbackImageUrl` en este contrato: el modelo del portfolio no lo tiene y la interfaz no lo inventa.

### 8. Navegación y rutas

Dos rutas, porque la navegación por pestañas del Navbar es basada en rutas: `NavTab.path` es la vía
única por la que la pestaña activa se resuelve (`resolveActiveNavigation` compara prefijos y gana la
más específica).

| Ruta | Pestaña | Page |
| --- | --- | --- |
| `/portfolio` | *Mi portfolio* (predeterminada de la sección) | `PortfolioSummaryPage` |
| `/portfolio/holdings` | *Mis tenencias* | `PortfolioHoldingsPage` |

En `app/navigation/sections.ts` la sección `portfolio` pasa de `path: null` a `path: "/portfolio"`, y sus
pestañas `portfolio-overview` y `portfolio-holdings` pasan a apuntar a sus rutas reales. Las pestañas
`portfolio-transactions` y `portfolio-stats` **conservan `path: null`**: permanecen deshabilitadas y
rotuladas como pendientes, conforme a DC-08 y a FR-002.

El Navbar no se modifica: recibe las secciones desde `app` y deriva el estado activo con el resolver
existente.

### 9. Formato de importes, porcentajes y fechas

La presentación vive en `utils/` como funciones puras, porque es una responsabilidad de la feature y no
del Service ni del Mapper.

| Función | Entrada | Salida |
| --- | --- | --- |
| `formatCredits` | centésimas `number \| null` | `10,00` · `-1,80` · `—` |
| `formatPercentage` | puntos porcentuales `number \| null` | `12,50 %` · `—` |
| `formatDateTime` | ISO 8601 `string \| null` | `20/10/2026 16:22` · `—` |

El formato numérico usa `Intl.NumberFormat` con locale `es-AR` y dos decimales fijos, de modo que los
miles y el separador decimal son los del español rioplatense. El guion `—` representa la ausencia de
dato y es visualmente distinto del `0,00`.

El signo se conserva en el valor (`-1,80`), y la distinguibilidad visual entre resultado positivo,
negativo y ausente no depende solo del color: el negativo lleva signo y los tres casos se distinguen
por el color del rol (`$state-positive`, `$state-negative`) y por el guion del ausente (FR-033).

### 10. Estados de la interfaz

Cada vista comunica cuatro estados, con el patrón ya usado en `PlayersPage`:

| Estado | Presentación | Rol ARIA |
| --- | --- | --- |
| Carga | «Cargando tu portfolio…» / «Cargando tus posiciones…» | `role="status"` |
| Vacío | Mensaje comprensible, distinto por vista y por filtro | `role="status"` |
| Error | Mensaje del backend si es válido y botón *Reintentar*, en el rol de error sobre superficie oscura | `role="alert"` |
| Contenido | La vista | `aria-busy` en el contenedor mientras carga |

El error usa `getErrorMessage(error, fallback)` de `infrastructure/http`, que devuelve el mensaje del
backend cuando es un `ApiError` válido y el texto de respaldo en cualquier otro caso. La feature no
interpreta códigos de error propios porque no tiene: el backend no define códigos funcionales para
esta consulta.

La diferencia entre el conjunto vacío y el filtro sin coincidencias se mantiene explícita: el filtro
*Todas* sin posiciones y el filtro *Cerradas* sin posiciones cerradas no muestran el mismo texto.

### 11. Recursos gráficos: reubicación del ícono genérico

El ícono genérico de jugador vive hoy en `features/players/assets/Player Generic-Icon.png` y lo
consumen `PlayerCard` y `PlayerPortrait`. Como una feature no puede depender de otra, el recurso se
**reubica** en `frontend/src/shared/assets/Player Generic-Icon.png` y ambos consumidores pasan a
importarlo desde allí.

La reubicación, y no la copia, evita dos versiones del mismo recurso que pueden divergir. No se crea
una ilustración nueva: la maquetaapproved muestra una silueta de persona, pero el requerimiento de
imágenes de jugadores ya establece el ícono genérico de FootballMarket como último recurso del
retrato, y esa convención se conserva.

Consecuencia documental: `docs/frontend/technologies.md` registra el inventario de recursos gráficos
locales y debe reflejar la nueva ubicación en el mismo cambio (Constitution §IV).

### 12. Estilos y tokens

Las tarjetas y el panel usan **superficies oscuras**, conforme a la referencia visual aprobada: el
fondo de la sección sigue el degradado azul del catálogo de jugadores y las superficies de tarjeta y
panel se apoyan en los neutros oscuros ya existentes (`$color-neutral-800`, `$color-neutral-950`).

Los estados financieros necesitan color sobre superficie oscura, y hoy no existe ninguno: el verde de
éxito, el rojo de error y el ámbar de advertencia del sistema están calculados para superficies claras
y no alcanzan contraste sobre `#1e293b`. Se nacen **cinco primitivos** en
`styles/abstracts/tokens/_colors.scss` y sus **cinco roles** semánticos en
`styles/abstracts/themes/_default.scss`:

| Primitivo | Valor | Rol | Uso | Contraste sobre `#1e293b` |
| --- | --- | --- | --- | --- |
| `$color-positive-on-dark` | `#4ade80` | `$state-positive` | Resultado positivo y acciones afirmativas | 8,40:1 (AAA) |
| `$color-negative-on-dark` | `#f87171` | `$state-negative` | Resultado negativo | 5,29:1 (AA) |
| `$color-caution-on-dark` | `#fbbf24` | `$state-caution` | «Sin cotización» y aviso de dato ausente | 8,76:1 (AAA) |
| `$color-error-on-dark` | `#fda4af` | `$state-error-on-dark` | Mensaje de error de la consulta | 7,74:1 (AA) |
| `$color-border-dark` | `#64748b` | `$border-surface-dark` | Borde de tarjeta y de panel sobre superficie oscura | 3,07:1 (borde) |

Los tres estados vigentes no sirven sobre superficie oscura: `$color-success` mide 1,96:1,
`$color-error` 2,02:1 y `$color-warning` 2,91:1 sobre `#1e293b`, todos por debajo del mínimo de 3:1.
El mensaje de error necesita su propio rol y no puede reutilizar `$state-negative`: un fallo de la
consulta y una pérdida de valor son hechos distintos y no deben compartir color.

El valor `#fda4af` del rol de error se fija por contraste: `#ef526e`, el rojo de la referencia, mide
4,25:1 sobre la tarjeta y **no** alcanza el mínimo de 4,5:1 que WCAG exige para texto normal. Con
`#fda4af` el mensaje de error mide 7,74:1 sobre la tarjeta, 8,85:1 sobre el fondo de página y 9,44:1
sobre el panel: AA con holgura en todas las superficies donde puede aparecer. Sigue siendo un valor
distinto del rojo del resultado negativo —1,46:1 entre ambos—, de modo que los dos roles no se
confunden; la prominencia del error la aportan el `role="alert"`, el borde del bloque y la acción de
reintento, no solo el tono.

**Delimitación de la tarjeta.** La superficie de tarjeta `#1e293b` sobre el fondo de página `#111b40`
mide 1,14:1: sin un límite explícito, la tarjeta no se distingue del fondo. La referencia visual lo
resuelve con un borde sutil (`#2b4466`, 1,69:1). Se adopta un borde propio
`$border-surface-dark` (`#64748b`), que mide 3,52:1 frente al fondo de página y 3,07:1 frente a la
propia tarjeta: la tarjeta se percibe con claridad y el límite cumple el mínimo de 3:1 que corresponde a
un elemento con significado. El valor `#64748b` ya existe en la paleta como
`$color-neutral-500`, con la función de texto secundario y separadores; se conserva como un primitivo
distinto porque su función es delimitar una superficie, y no una separación de texto.

El valor verde coincide con `$color-accent-green` a propósito: la aplicación mantiene **un solo verde**,
el de la navegación activa, y la sección Cartera no introduce un segundo verde. Los cinco roles nuevos
se registran con su función y su contraste medido sobre la superficie oscura, como exige el archivo de
tokens.

Los componentes no escriben valores cromáticos literales: consumen los roles semánticos (FR-031). Las
dimensiones de la retícula de tres columnas usan los breakpoints existentes (`$bp-grid-md` para tres
columnas, `$bp-grid-sm` para dos y `$bp-xs` para una), y la readaptación a dos o una columnas en
pantallas estrechas no cambia el tamaño de página, conforme a FR-034.

### 13. Panel lateral de detalle

El panel se superpone al contenido de la pestaña, que permanece visible detrás e inerte (FR-020). Se
implementa como un diálogo modal accesible:

- `role="dialog"` y `aria-modal="true"`, con `aria-labelledby` apuntando a su título.
- Al abrir, el foco pasa al panel; al cerrar, vuelve al botón *Ver detalles* que la abrió.
- La tecla `Escape` cierra el panel.
- El contenido de la pestaña marcado como inerte mientras el panel está abierto, de modo que ni el
  filtro ni la paginación sean alcanzables con el teclado ni con el puntero.
- El panel se renderiza sobre la grilla sin alterar el filtro, la página ni el orden (FR-023).

El contenido del panel se construye con la posición ya cargada en la página: **no hay una segunda
consulta** (FR-022). El contrato expone los 26 campos de la posición y el panel los presenta
organizados en las cinco secciones de la referencia: identificación, tenencia, inversión y
valoración, actividad y resultados.

**Comportamiento en pantalla estrecha.** La misma semántica de diálogo modal se conserva en todos los
anchos; lo que cambia es su colocación:

| Condición | Comportamiento |
| --- | --- |
| Ancho ≥ 900 px | Panel lateral anclado al borde derecho, de ancho fijo, que deja visible una porción de la grilla. La referencia lo dibuja con 700 px sobre 1635 px de lienzo, algo más del 40 % del ancho |
| Ancho < 900 px | El panel ocupa **todo** el ancho del área de contenido y toda la altura disponible, sin dejar visible la grilla |

En ambos casos, y con independencia del ancho:

- La cabecera del panel —título y control de cierre— permanece **fija y visible**: no se desplaza con
  el contenido.
- El cuerpo del panel es la única zona con desplazamiento propio, y el panel **nunca excede la altura
  disponible**: su contenido se desplaza dentro de él, nunca el documento.
- El control de cierre mantiene su nombre accesible, sigue siendo alcanzable con el teclado y mantiene
  su área táctil en pantalla estrecha.
- El foco queda confinado dentro del panel, `Escape` lo cierra y la restitución del foco al botón que
  lo abrió se produce igual.
- El contenido de la pestaña permanece inerte, también cuando el panel ocupa todo el ancho.

El corte de 900 px reutiliza el breakpoint existente de una sola columna (`$bp-detail`): por debajo de
ese ancho no hay ancho suficiente para un panel lateral legible, y la solución de ancho completo es la
que mantiene el contenido y la acción de cierre accesibles sin renunciar a la semántica modal.

### 14. Rendimiento

- Una solicitud por vista; ninguna por posición ni por jugador.
- La consulta del resumen usa el tamaño mínimo admitido, porque el resumen no depende del tamaño.
- La caché de TanStack Query (30 s) evita repetir la consulta al alternar pestañas.
- Las imágenes del retrato cargan de forma diferida por el navegador y degradan al ícono genérico si
  fallan, sin bloquear el renderizado del resto de la tarjeta.
- El panel de detalle no dispara solicitudes: se construye con datos ya disponibles.

### 15. Fiabilidad y manejo de errores

| Situación | Comportamiento |
| --- | --- |
| Sesión ausente o vencida (401) | La cadena de seguridad del `infrastructure/http` resuelve la sesión y la aplicación vuelve al acceso público. No se muestra información financiera. |
| Error del backend (500, 503) | Estado de error con el mensaje del backend y botón *Reintentar*. No se muestra ningún dato financiero parcial. |
| Cotizaciones no disponibles (503 `QUOTE_SOURCE_UNAVAILABLE`) | Es el caso anterior: el backend no devuelve datos parciales y la interfaz tampoco los compone. |
| Cotización ausente (200 con valoración incompleta) | **No es un error**: la posición muestra *Sin cotización* y los indicadores afectados muestran el guion. |
| Red caída o error de transporte | Error genérico con reintento. No se incorpora un validador de respuestas en el Mapper. |

### 16. Accesibilidad

- El contenedor de cada vista expone `aria-busy` mientras carga.
- La grilla de posiciones es una lista semántica (`ul`/`li`) con `aria-label`, para que el número de
  posiciones sea percibido como tal.
- Carga y vacío se anuncian con `role="status"`; el error, con `role="alert"`.
- El panel de detalle es un diálogo modal con gestión de foco (§13), cuyo comportamiento en pantalla
  estrecha —ancho completo, cabecera fija y desplazamiento propio del cuerpo— mantiene la misma
  semántica de foco, de cierre con `Escape` y de contenido de fondo inerte.
- Los controles del filtro son botones con `aria-pressed` según el estado activo.
- El resultado positivo, el negativo y el ausente se distinguen por signo y por texto, no solo por
  color (FR-033).
- El ícono genérico lleva texto alternativo que no depende de la imagen para identificar al jugador,
  y el nombre del jugador permanece visible cuando está disponible. `playerName`, `teamName` y
  `position` admiten `null`; se indica el dato ausente sin inventarlo. El retrato admite nombre nulo
  y conserva un texto alternativo genérico; la posición y sus cifras siguen consultables.

### 17. Estrategia de pruebas

El frontend está exento de testing de forma temporal (Constitution y `frontend/AGENTS.md`), así que
**esta feature no crea tests ni infraestructura de testing**. La estrategia de validación es:

1. **Control ejecutable permitido**: `npm run build` desde `frontend/`, que ejecuta el type-check de
   TypeScript en modo estricto y el build de Vite. Lo ejecuta el usuario.
2. **Comprobación manual guiada**: los escenarios de [`quickstart.md`](quickstart.md), que recorren las
   tres historias, los estados de la interfaz, la ausencia de cotización, la posición cerrada, el
   filtro, la paginación, el panel de detalle y la comprobación de la responsive.
3. **Revisión de contrato**: contraste entre lo que la interfaz presenta y lo que devuelve
   `GET /api/portfolio/me`, verificando que ninguna cifra se recalcula y que ninguna ausencia se
   muestra como cero.

### 18. Riesgos y decisiones arquitectónicas

| ID | Riesgo o decisión | Mitigación |
| --- | --- | --- |
| RA-01 | La reubicación del ícono genérico toca la feature de jugadores | Es un cambio mecánico de dos imports y un `git mv`; no altera el comportamiento observable del catálogo ni de la ficha. Se acompaña de la actualización del inventario de recursos en `technologies.md`. |
| RA-02 | Los cinco roles nuevos nacen en el sistema de diseño global | Se justifican por un uso real, se documentan con su contraste medido sobre la superficie oscura y no se crean roles especulativos. El archivo de tokens exige precisamente este procedimiento. |
| RA-03 | El resumen y las tenencias son dos entradas de caché distintas sobre el mismo endpoint | Es deliberado: una clave única que variara con la página invalidaría el resumen al paginar. El costo —una solicitud por vista— es aceptable y está cubierto por el objetivo de rendimiento. |
| RA-04 | El resumen se pide con `size=1` | El contrato garantiza que el resumen representa el portfolio completo con independencia del filtro y de la página. El cliente confía en esa garantía sin recalcular ni revalidar la paginación. |
| RA-05 | El backend puede responder con una página vacía si se pide una página fuera de rango | El Page Hook recorta a la última página válida, siguiendo el patrón de `usePlayersPage`. |
| RA-06 | El panel de detalle depende de la posición cargada en la página | Es lo que exige FR-022. Si la posición se recargara por un refresco, el panel se reabriría con la posición nueva; el panel no conserva estado propio de la posición. |
| RA-07 | No hay objetivos de rendimiento del frontend definidos en el proyecto | No se inventan: el plan se limita a lo verificable (una solicitud por vista, reutilización de caché, degradación de imagen) y no introduce SLA numérico. |

## Archivos que deberán modificarse durante IMPLEMENT

| Archivo | Cambio | Alcance |
| --- | --- | --- |
| `frontend/src/app/navigation/sections.ts` | Habilitar la sección `portfolio` y sus dos pestañas | App |
| `frontend/src/app/router/AppRouter.tsx` | Registrar `/portfolio` y `/portfolio/holdings` | App |
| `frontend/src/features/portfolio/**` | Feature nueva completa | Feature |
| `frontend/src/shared/assets/Player Generic-Icon.png` | Reubicación desde `features/players/assets/` | Shared |
| `frontend/src/features/players/components/PlayerCard/PlayerCard.tsx` | Apuntar al ícono reubicado | Feature existente |
| `frontend/src/features/players/components/PlayerPortrait/PlayerPortrait.tsx` | Apuntar al ícono reubicado | Feature existente |
| `frontend/src/styles/abstracts/tokens/_colors.scss` | Tres primitivos de estado sobre superficie oscura | Estilos globales |
| `frontend/src/styles/abstracts/themes/_default.scss` | Tres roles semánticos de estado financiero | Estilos globales |
| `docs/frontend/technologies.md` | Actualizar el inventario de recursos gráficos locales | Documentación |

No se modifica ningún archivo del backend, ni `tp/`, ni los artefactos de otras SPEC.

## Verificación de alineación con SPEC-017

| Requisito | Elemento del diseño |
| --- | --- |
| FR-001, FR-002 | §8: sección `portfolio` habilitada con `path` y dos pestañas con ruta; las otras dos conservan `path: null`. |
| FR-003 | §6: la vista no expone acciones de mercado; el modelo no incluye ninguna operación. |
| FR-004, FR-005, FR-006 | §3 y §9: los importes se modelan en centésimas y se presentan tal como llegan; el Mapper no calcula; el formato vive en `utils/`. |
| FR-007 | §2: la solicitud no incluye identificador de usuario; `authenticated: true` resuelve la identidad desde la sesión. |
| FR-008, FR-009, FR-010 | §7: `PortfolioMetricCard` × 4 y `PortfolioValueSection` × 2 cubren los trece indicadores; el indicador de posiciones se rotula como abiertas (DC-09). |
| FR-011 | §7 y §8: acción *Ver mis tenencias* que navega a `/portfolio/holdings`. Se entrega junto con la segunda pestaña, en el mismo incremento en que esa ruta queda disponible |
| FR-012 | §10: estado vacío de la vista de resumen. |
| FR-013, FR-014 | §7: `PortfolioPositionCard`; §5: `size=6`. |
| FR-015 | §5, §6 y §10: metadatos de paginación del backend; controles presentes aun con una sola página. |
| FR-016, FR-017 | §6: el filtro se traduce a `status`; el reinicio a la primera página ocurre en el Page Hook. |
| FR-018 | §3: el Mapper conserva el orden recibido. |
| FR-019 | §10: estado vacío del conjunto filtrado, distinto del vacío global. |
| FR-020, FR-021, FR-022, FR-023, FR-024, FR-025 | §13: panel superpuesto, inerte y accesible; contenido desde la posición ya cargada; cierre sin alterar la grilla. |
| FR-026 a FR-030 | §10: los cuatro estados; §11: cadena `imageUrl` → ícono genérico; §15: ausencia contra error. |
| FR-031, FR-032 | §12: roles semánticos y disposición de la referencia aprobada. |
| FR-033 | §9: signo conservado yroles diferenciados. |
| FR-034 | §12: readaptación de columnas con tamaño de página constante. |
| FR-035 | §13: diálogo modal, foco gestionado y `Escape`. |
| FR-036 | §12: coherencia con el tema y los estilos vigentes. |
| FR-037, FR-038, FR-039 | Fuera de alcance: no hay historial, ni gráficos, ni datos de demostración. |
| DC-10 | §9 y §10: el texto de interfaz usa tuteo. |
| SC-001 | §14: una solicitud por vista, sin peticiones por posición. |
| SC-002 | §7: los trece indicadores presentes sin paginación ni diálogo. |
| SC-003, SC-004 | §9: conversión a créditos con dos decimales y ausencia explícita. |
| SC-005 | §13: el detalle se alcanza con una acción y se cierra con una. |
| SC-006 | §6: el filtro se traduce al parámetro `status`. |
| SC-007 | §6: el resumen tiene clave de caché propia y no depende de la página ni del filtro. |
| SC-008 | §10 y §15: mensaje de error y reintento sin datos parciales. |
| SC-009, SC-010 | §12 y §16: breakpoints existentes, semántica de lista y gestión de foco. |
| SC-011 | §7 y DC-09: rótulo explícito de posiciones abiertas frente al total listado. |

## Estado de Git

- Rama: `feat/017-view-personal-portfolio-ui`, creada desde `origin/dev` y publicada y vinculada al
  issue #26.
- Base de la rama: `ad3699103e7bac394b055dd3b1e0af916858e90e`.
- Sin commits propios: este plan no se commitea. La rama permanece en el mismo commit que `origin/dev`
  hasta que una operación autorizada la modifique.

## Siguiente paso

`/speckit.checklist` — los checklists de calidad son obligatorios antes de `/speckit.tasks`. Este plan
no habilita la implementación: la autorización de implementación sigue requiriendo un gate humano
explícito y vigente.

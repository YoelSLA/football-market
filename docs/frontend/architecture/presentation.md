# Organización visual y recursos del frontend

Encapsulación de Components de Page, estilos, distribución de alturas y recursos compartidos. No reemplaza las convenciones de implementación.

Consultar el [punto de entrada](../architecture.md), la [interpretación normativa única](../architecture.md#interpretacion-normativa) y las [observaciones de consistencia](../architecture.md#consistencia). Las matrices de [dependencias](dependencies.md) se consultan junto con el rol afectado.

## Índice

- [Artículo 3.º — Ubicación de Components de página](#articulo-3)
- [Artículo 4.º — Delegación visual y Components locales](#articulo-4)
- [Artículo 5.º — Imports del Design System](#articulo-5)
- [Artículo 31.º — Cadena de altura](#articulo-31)
- [Artículo 32.º — Fondos y responsabilidad visual](#articulo-32)
- [Artículo 33.º — Scroll y responsive](#articulo-33)
- [Artículo 34.º — Prevención de regresiones visuales](#articulo-34)
- [Artículo 35.º — API única de iconos](#articulo-35)
- [Artículo 36.º — Capas globales de estilos](#articulo-36)
- [Artículo 37.º — Procedencia y tolerancia a fallos de recursos](#articulo-37)

## CAPÍTULO PRIMERO — Components locales e imports

<a id="articulo-3"></a>
### Artículo 3.º — Ubicación de Components de página

**MUST:** Los Components creados exclusivamente para una Page se ubican **dentro de la carpeta de esa Page** (`pages/<Page>/<Component>/`).

**MUST NOT:** Esos Components se colocan en `features/<feature>/components/`.

**MUST:** La carpeta `components/` de la feature queda reservada para Components **compartidos entre distintas Pages o funcionalidades** de la feature.

**MUST NOT:** Se mueven Components compartidos existentes a carpetas locales si otros consumidores los utilizan.

**MUST:** La colocalización es jerárquica y recursiva en todo `frontend/src/`, incluidos `app`, layouts, providers y `shared`. Un Component privado de otro reside dentro de la carpeta de su propietario. Un Component compartido entre sus hijos permanece en el nivel común más cercano; entre Pages, en `components/` de la feature; entre features, en `shared/components/` únicamente si es genérico. Se comprueban consumidores efectivos y responsabilidad de composición: un único importador no demuestra por sí solo propiedad. Las fronteras arquitectónicas prevalecen sobre el anidamiento físico; por ejemplo, la UI de sesión sigue en auth aunque la consuma el guard global.

**MUST:** Cada Component local de Page tiene su propia carpeta con:
- Archivo TSX de implementación
- SCSS Module propio
- `index.ts` barrel para exportar su API pública

Esta regla refuerza la encapsulación y cohesión: los componentes de página viajan junto a la página que los usa, y su ciclo de vida está ligado a ella.

Ejemplo ilustrativo: véase el [Anexo A](examples.md#anexo-a).

<a id="articulo-4"></a>
### Artículo 4.º — Delegación visual y Components locales

**MUST:** Las Pages se encargan principalmente de:

- Componer componentes.
- Conectar hooks.
- Configurar layouts.
- Coordinar las Props de sus componentes hijos.

**MUST:** Cuando una Page tiene responsabilidades visuales diferenciadas, las delega a Components locales propios (dentro de `pages/<Page>/`). Criterios de extracción:

- **Headers** complejos con controles y estado.
- **Listados y grids** con lógica de renderizado condicional.
- **Secciones de contenido** independientes (métricas, paneles, formularios).
- **Estados de carga, error y vacío** cuando tienen UI propia.

**MUST NOT:** Se extraen elementos triviales sin responsabilidad clara.

Evitar sobrecomponentización es el propósito de esta restricción.

**MUST:** Los Components locales de Page siguen SRP (responsabilidad única): cada uno gestiona una preocupación visual diferenciada y recibe datos mediante Props.

**MUST NOT:** Los Components locales de Page gestionan estados de carga/error de la Page o conectan Hooks directamente.

<a id="articulo-5"></a>
### Artículo 5.º — Imports del Design System

**MUST:** Todos los archivos SCSS que importen los abstracts del design system utilizan **exclusivamente** el alias `@`.

**MUST NOT:** Se utilizan imports relativos para esos abstracts.

Esta regla aplica a **todos** los archivos `.scss` bajo `frontend/src/`. El alias `@` está configurado en `tsconfig.json` y `vite.config.ts` y resuelve a `src/`.

La forma canónica es `@use "@/styles/abstracts" as ds;`.

Ejemplos ilustrativos: véase el [Anexo B](examples.md#anexo-b).

## CAPÍTULO SEGUNDO — Layouts y distribución visual

<a id="articulo-31"></a>
### Artículo 31.º — Cadena de altura

Para garantizar un comportamiento consistente y prevenir regresiones visuales en las páginas que usan `PageLayout` y `PaginatedPageLayout`, se establecen las siguientes reglas:

Cadena de altura (height chain):

a) **MUST:** `AppLayout` fija la altura al viewport (`height: 100vh / 100dvh`) y oculta el overflow del documento.

b) **MUST:** `AppLayout__content` es un flex container columna con `flex: 1`, `min-height: 0`, `overflow: hidden`.

c) **MUST:** Cada Page contenedora (wrapper) tiene `display: flex`, `flex: 1`, `flex-direction: column`, `min-height: 0`, `min-width: 0` para participar en la cadena de altura.

d) **MUST:** `PageLayout` (`.layout`) recibe `flex: 1` y `flex-direction: column`; reparte el espacio entre header (flex-shrink: 0), content (flex: 1) y footer (flex-shrink: 0).

e) **MUST:** `PageLayout__content` es el **único contenedor con scroll vertical** en desktop (`overflow-y: auto`, `min-height: 0`) y es transparente para heredar el fondo del wrapper de la Page.

f) **MUST NOT:** `PageLayout__content` tiene fondo propio.

<a id="articulo-32"></a>
### Artículo 32.º — Fondos y responsabilidad visual

a) **MUST:** El fondo de Page (sólido, gradiente o imagen) se declara en el wrapper de la Page.

b) **MUST NOT:** `PageLayout` y sus elementos internos (header, content, footer) definen fondos propios; son transparentes para dejar ver el fondo del wrapper.

c) **MUST:** En modo compacto (`@include ds.compact()`), el footer flotante (paginación capsule) tiene su propio fondo semitransparente con `backdrop-filter`; no hereda el fondo de Page.

Ejemplos ilustrativos de wrappers: véase el [Anexo A](examples.md#anexo-a).

<a id="articulo-33"></a>
### Artículo 33.º — Scroll y responsive

a) **MUST:** En desktop (≥ tablet), el scroll vertical vive exclusivamente en `PageLayout__content`. El documento no scrollea.

b) **MUST:** En móvil (< tablet), `PageLayout__content` pasa a `overflow: visible`; el scroll vuelve al documento natural.

c) **MUST:** El modo compacto se activa solo con viewport ancho (`≥ $bp-catalog-compact`) **y** alto (`≥ $bp-compact-height`). El footer se ancla absolutamente y el contenido reserva su banda (`padding-bottom: $pagination-band`).

<a id="articulo-34"></a>
### Artículo 34.º — Prevención de regresiones visuales

a) **MUST NOT:** Se agrega `background-color` en `PageLayout__content` o en `.layout__header`/`.layout__footer`.

b) **MUST NOT:** Se usan alturas fijas (`height: Xpx`) en contenedores de Page.

c) **MUST:** Se utiliza la cadena de flex con `min-height: 0`. Al crear una nueva Page con `PageLayout` o `PaginatedPageLayout`, el wrapper sigue la cadena de altura (`flex: 1`, `flex-direction: column`, `min-height: 0`).

d) **MUST:** Se verifica visualmente en desktop, tablet, móvil y modo compacto tras cualquier cambio en layouts compartidos.

## CAPÍTULO TERCERO — Estilos y recursos gráficos

<a id="articulo-35"></a>
### Artículo 35.º — API única de iconos

**MUST:** `shared/icons` es el **punto único de exportación de los iconos** de la aplicación. Reexporta desde `lucide-react` cada icono que usa Football Market, junto con el tipo `LucideIcon`. Todos los consumidores utilizan `@/shared/icons`. Cada icono se exporta una sola vez con su nombre canónico y es el consumidor quien le da significado mediante su etiqueta, su texto alternativo y su rol en la interfaz.

**MUST NOT:** Los consumidores importan de `lucide-react` directamente. El módulo `shared/icons` importa de `app` o de `features`.

Así, el inventario de iconos queda declarado en un solo lugar, un cambio de biblioteca se resuelve en ese módulo y no en cada consumidor, y el módulo no introduce ciclos.

<a id="articulo-36"></a>
### Artículo 36.º — Capas globales de estilos

**MUST:** `src/styles` se organiza en dos capas globales: `styles/base/` con el reset y los estilos elementales, y `styles/abstracts/` con los símbolos compartidos —`breakpoints`, `tokens/` (colores, espaciado, layout) y `themes/`—. `styles/index.scss` es el único punto de entrada global y delega el reset en `base/`. Los valores de presentación se consumen desde `abstracts`.

Los símbolos compartidos se consumen mediante `@use`. La exigencia del alias se conserva en el [Artículo 5.º](#articulo-5); véase el [Anexo B](examples.md#anexo-b) para las notaciones ilustrativas.

La divergencia documentada sobre colores se conserva en [Estilos de la implementación](implementation.md#estilos-de-la-implementacion).

<a id="articulo-37"></a>
### Artículo 37.º — Procedencia y tolerancia a fallos de recursos

**MUST:** Se documentan ambas procedencias de los recursos gráficos decorativos:

a) **Recursos locales:** importados en tiempo de build y empaquetados por Vite.

b) **Servicios externos:** consultados en runtime.

**MUST:** Los servicios externos tienen marcador de posición accesible y comportamiento tolerante a fallos conforme a `tp/enunciado.md` §8.

Ninguno de los dos constituye un contrato de datos ni una ampliación de la API del backend.

Procedencias concretas: véase [Recursos de la implementación](implementation.md#recursos-de-la-implementacion).
